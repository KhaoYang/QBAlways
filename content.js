(() => {
  const HOST_ID = "qbalways-selection-search";
  const MAX_SELECTION_LENGTH = 300;
  let selectedText = "";
  let host;
  let button;

  function ensureButton() {
    if (host) return;

    host = document.createElement("div");
    host.id = HOST_ID;
    host.style.position = "fixed";
    host.style.zIndex = "2147483647";
    host.style.display = "none";
    host.style.left = "0";
    host.style.top = "0";

    const shadow = host.attachShadow({ mode: "closed" });
    const style = document.createElement("style");
    style.textContent = `
      button {
        align-items: center;
        background: #f1c75b;
        border: 1px solid #6e5211;
        border-radius: 999px;
        box-shadow: 0 4px 14px rgb(20 17 10 / 28%);
        color: #211a09;
        cursor: pointer;
        display: inline-flex;
        font: 700 12px/1 system-ui, sans-serif;
        gap: 5px;
        height: 30px;
        padding: 0 10px;
        white-space: nowrap;
      }
      button:hover { background: #ffda72; transform: translateY(-1px); }
      button:focus-visible { outline: 3px solid #3874ff; outline-offset: 2px; }
      .mark { font-size: 13px; }
    `;

    button = document.createElement("button");
    button.type = "button";
    button.title = "Search selected text in QBReader";
    button.setAttribute("aria-label", "Search selected text in QBReader");
    const mark = document.createElement("span");
    mark.className = "mark";
    mark.textContent = "Q";
    const label = document.createElement("span");
    label.textContent = "Search QBReader";
    button.append(mark, label);
    button.addEventListener("mousedown", (event) => event.preventDefault());
    button.addEventListener("click", (event) => {
      event.preventDefault();
      event.stopPropagation();
      if (!selectedText) return;
      chrome.runtime.sendMessage({ type: "searchSelection", text: selectedText });
      hideButton();
    });

    shadow.append(style, button);
    document.documentElement.append(host);
  }

  function showButtonForSelection() {
    const selection = window.getSelection();
    const text = selection?.toString().replace(/\s+/g, " ").trim();
    if (!selection || selection.isCollapsed || !text) {
      hideButton();
      return;
    }

    const range = selection.getRangeAt(0);
    const rect = range.getBoundingClientRect();
    if (!rect.width && !rect.height) {
      hideButton();
      return;
    }

    ensureButton();
    selectedText = text.slice(0, MAX_SELECTION_LENGTH);
    host.style.display = "block";

    const buttonWidth = button.getBoundingClientRect().width || 132;
    const left = Math.min(
      Math.max(8, rect.left + rect.width / 2 - buttonWidth / 2),
      window.innerWidth - buttonWidth - 8
    );
    const preferredTop = rect.bottom + 8;
    const top = preferredTop + 38 < window.innerHeight
      ? preferredTop
      : Math.max(8, rect.top - 38);

    host.style.left = `${left}px`;
    host.style.top = `${top}px`;
  }

  function hideButton() {
    selectedText = "";
    if (host) host.style.display = "none";
  }

  document.addEventListener("mouseup", () => setTimeout(showButtonForSelection, 0));
  document.addEventListener("keyup", (event) => {
    if (event.key === "Escape") hideButton();
    if (event.key.startsWith("Arrow") || event.key === "Shift") {
      setTimeout(showButtonForSelection, 0);
    }
  });
  document.addEventListener("mousedown", (event) => {
    if (host && event.composedPath().includes(host)) return;
    hideButton();
  });
  window.addEventListener("scroll", hideButton, { passive: true });
})();
