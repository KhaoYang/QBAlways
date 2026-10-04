const PLATFORM_API_URL = "http://localhost:8080/api/search";
const QBREADER_API_URL = "https://www.qbreader.org/api/query";
const PAGE_SIZE = 10;

const form = document.querySelector("#search-form");
const queryInput = document.querySelector("#query");
const questionType = document.querySelector("#question-type");
const searchType = document.querySelector("#search-type");
const exactPhrase = document.querySelector("#exact-phrase");
const submitButton = form.querySelector('button[type="submit"]');
const status = document.querySelector("#status");
const results = document.querySelector("#results");

let activeRequest;
let lastRequestedAt = 0;

form.addEventListener("submit", (event) => {
  event.preventDefault();
  search(queryInput.value);
});

chrome.storage.onChanged.addListener((changes, areaName) => {
  if (areaName !== "session" || !changes.pendingSearch?.newValue) return;
  consumePendingSearch(changes.pendingSearch.newValue);
});

initialize();

async function initialize() {
  const { pendingSearch } = await chrome.storage.session.get("pendingSearch");
  if (pendingSearch) consumePendingSearch(pendingSearch);
}

function consumePendingSearch(pendingSearch) {
  if (!pendingSearch.text || pendingSearch.requestedAt <= lastRequestedAt) return;
  lastRequestedAt = pendingSearch.requestedAt;
  queryInput.value = pendingSearch.text;
  search(pendingSearch.text);
}

async function search(rawQuery) {
  const query = rawQuery.replace(/\s+/g, " ").trim();
  if (!query) return;

  activeRequest?.abort();
  activeRequest = new AbortController();
  setLoading(true);
  setStatus(`Searching for "${truncate(query, 80)}"...`);
  results.replaceChildren();

  try {
    const platformResult = await searchPlatform(query, activeRequest.signal);
    if (platformResult.total > 0) {
      renderPlatformResults(platformResult, query);
      return;
    }

    const qbReaderResult = await searchQbReader(query, activeRequest.signal);
    renderQbReaderResults(qbReaderResult, query, "The local index had no matches; showing QBReader results.");
  } catch (platformError) {
    if (platformError.name === "AbortError") return;

    try {
      const qbReaderResult = await searchQbReader(query, activeRequest.signal);
      renderQbReaderResults(qbReaderResult, query, "Local search is offline; showing QBReader results.");
    } catch (fallbackError) {
      if (fallbackError.name === "AbortError") return;
      setStatus("Search could not be completed. Check the local services or try again.", true);
    }
  } finally {
    setLoading(false);
  }
}

async function searchPlatform(query, signal) {
  const params = new URLSearchParams({
    q: query,
    type: questionType.value.toUpperCase(),
    within: searchType.value.toUpperCase(),
    exact: String(exactPhrase.checked),
    size: String(PAGE_SIZE * 2)
  });
  const response = await fetch(`${PLATFORM_API_URL}?${params}`, { signal });
  if (!response.ok) throw new Error(`Search platform returned ${response.status}`);
  return response.json();
}

async function searchQbReader(query, signal) {
  const params = new URLSearchParams({
    q: query,
    questionType: questionType.value,
    searchType: searchType.value,
    exactPhrase: String(exactPhrase.checked),
    maxReturnLength: String(PAGE_SIZE)
  });
  const response = await fetch(`${QBREADER_API_URL}?${params}`, { signal });
  if (!response.ok) throw new Error(`QBReader returned ${response.status}`);
  return response.json();
}

function renderPlatformResults(data, query) {
  setStatus(`${formatCount(data.total, "ranked match")} - ${data.tookMs} ms - Solr index`);
  renderSection("Ranked results", data.results ?? [], data.total, query, true);
}

function renderQbReaderResults(data, query, notice = "") {
  const tossups = data.tossups?.questionArray ?? [];
  const bonuses = data.bonuses?.questionArray ?? [];
  const tossupCount = data.tossups?.count ?? 0;
  const bonusCount = data.bonuses?.count ?? 0;
  const counts = `${formatCount(tossupCount, "tossup")} - ${formatCount(bonusCount, "bonus")}`;

  setStatus(notice ? `${notice} ${counts}` : counts);

  if (!tossups.length && !bonuses.length) {
    renderEmpty();
    return;
  }

  if (tossups.length) renderSection("Tossups", tossups, tossupCount, query, false, "tossup");
  if (bonuses.length) renderSection("Bonuses", bonuses, bonusCount, query, false, "bonus");
}

function renderSection(title, items, total, query, isPlatform, fallbackKind) {
  if (!items.length) {
    renderEmpty();
    return;
  }

  const heading = document.createElement("h2");
  heading.className = "section-heading";
  heading.append(document.createTextNode(title));
  const count = document.createElement("span");
  count.textContent = `showing ${items.length} of ${total.toLocaleString()}`;
  heading.append(count);
  results.append(heading);

  for (const item of items) {
    const kind = isPlatform ? item.questionType : fallbackKind;
    results.append(createResultCard(item, kind, query, isPlatform));
  }
}

function renderEmpty() {
  const empty = document.createElement("div");
  empty.className = "empty";
  empty.textContent = "No matches found. Try turning off phrase matching or selecting fewer words.";
  results.append(empty);
}

function createResultCard(item, kind, query, isPlatform) {
  const card = document.createElement("article");
  card.className = "result-card";

  const meta = document.createElement("div");
  meta.className = "result-meta";
  const badge = document.createElement("span");
  badge.className = "kind";
  badge.textContent = kind;
  const source = document.createElement("span");
  source.textContent = isPlatform ? buildPlatformSource(item) : buildQbReaderSource(item);
  meta.append(badge, source);

  const body = document.createElement("div");
  body.className = "result-body";
  const question = document.createElement("p");
  question.className = "question";
  const bodyText = isPlatform ? item.questionText ?? "" : questionText(item, kind);
  appendHighlightedText(question, excerpt(bodyText, query), query);

  const answer = document.createElement("p");
  answer.className = "answer";
  const answerValue = isPlatform ? item.answerText ?? "" : answerText(item, kind);
  appendHighlightedText(answer, answerValue, query);

  body.append(question, answer);
  card.append(meta, body);
  return card;
}

function questionText(item, kind) {
  if (kind === "tossup") return item.question_sanitized ?? "";
  return [item.leadin_sanitized, ...(item.parts_sanitized ?? [])].filter(Boolean).join(" ");
}

function answerText(item, kind) {
  if (kind === "tossup") return item.answer_sanitized ?? "";
  return (item.answers_sanitized ?? []).join(" / ");
}

function buildPlatformSource(item) {
  return [
    item.setName,
    item.packetName ? `Packet ${item.packetName}` : null,
    item.category,
    Number.isFinite(item.difficulty) ? `Difficulty ${item.difficulty}` : null
  ].filter(Boolean).join(" - ");
}

function buildQbReaderSource(item) {
  return [
    item.set?.name,
    item.packet?.name ? `Packet ${item.packet.name}` : null,
    item.category,
    Number.isFinite(item.difficulty) ? `Difficulty ${item.difficulty}` : null
  ].filter(Boolean).join(" - ");
}

function excerpt(text, query) {
  const compact = text.replace(/\s+/g, " ").trim();
  const index = compact.toLocaleLowerCase().indexOf(query.toLocaleLowerCase());
  if (compact.length <= 430) return compact;
  if (index < 0) return `${compact.slice(0, 427)}...`;

  const start = Math.max(0, index - 155);
  const end = Math.min(compact.length, index + query.length + 245);
  return `${start ? "..." : ""}${compact.slice(start, end)}${end < compact.length ? "..." : ""}`;
}

function appendHighlightedText(element, text, query) {
  if (!query) {
    element.textContent = text;
    return;
  }

  const lowerText = text.toLocaleLowerCase();
  const lowerQuery = query.toLocaleLowerCase();
  let cursor = 0;
  let index = lowerText.indexOf(lowerQuery);

  while (index !== -1) {
    element.append(document.createTextNode(text.slice(cursor, index)));
    const mark = document.createElement("mark");
    mark.textContent = text.slice(index, index + query.length);
    element.append(mark);
    cursor = index + query.length;
    index = lowerText.indexOf(lowerQuery, cursor);
  }

  element.append(document.createTextNode(text.slice(cursor)));
}

function setStatus(message, isError = false) {
  status.textContent = message;
  status.classList.toggle("error", isError);
}

function setLoading(isLoading) {
  submitButton.disabled = isLoading;
  submitButton.textContent = isLoading ? "Searching..." : "Search";
}

function formatCount(count, label) {
  return `${count.toLocaleString()} ${label}${count === 1 ? "" : "s"}`;
}

function truncate(text, length) {
  return text.length > length ? `${text.slice(0, length - 3)}...` : text;
}
