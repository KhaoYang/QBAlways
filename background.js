const MENU_ID = "qbalways-search-selection";
const MAX_SELECTION_LENGTH = 300;

chrome.runtime.onInstalled.addListener(() => {
  chrome.contextMenus.removeAll(() => {
    chrome.contextMenus.create({
      id: MENU_ID,
      title: 'Search QBReader for “%s”',
      contexts: ["selection"]
    });
  });

  chrome.sidePanel
    .setPanelBehavior({ openPanelOnActionClick: true })
    .catch((error) => console.error("Could not configure the side panel", error));
});

chrome.runtime.onStartup.addListener(() => {
  chrome.sidePanel
    .setPanelBehavior({ openPanelOnActionClick: true })
    .catch((error) => console.error("Could not configure the side panel", error));
});

chrome.contextMenus.onClicked.addListener((info, tab) => {
  if (info.menuItemId !== MENU_ID || !info.selectionText || !tab?.id) return;
  queueSearch(info.selectionText, tab.id);
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message?.type !== "searchSelection" || !sender.tab?.id) return;

  queueSearch(message.text, sender.tab.id)
    .then(() => sendResponse({ ok: true }))
    .catch((error) => sendResponse({ ok: false, error: error.message }));

  return true;
});

async function queueSearch(rawText, tabId) {
  const text = normalizeSelection(rawText);
  if (!text) return;

  // Start opening synchronously while Chrome still recognizes the click as a
  // user gesture. The storage listener will deliver the query to an open panel.
  const openPanel = chrome.sidePanel.open({ tabId });
  const saveSearch = chrome.storage.session.set({
    pendingSearch: {
      text,
      requestedAt: Date.now()
    }
  });

  await Promise.all([openPanel, saveSearch]);
}

function normalizeSelection(value) {
  return String(value ?? "")
    .replace(/\s+/g, " ")
    .trim()
    .slice(0, MAX_SELECTION_LENGTH);
}
