# QBAlways

QBAlways is a Manifest V3 Chrome extension that searches selected webpage text in the [QBReader](https://www.qbreader.org/) question database.

## Install locally

1. Open `chrome://extensions` in Chrome.
2. Turn on **Developer mode**.
3. Click **Load unpacked**.
4. Choose this `QBAlways` folder.

## Use it

Select text on a webpage and click the floating **Search QBReader** button. The matching tossups and bonuses open in Chrome's side panel. You can also right-click selected text and choose **Search QBReader for…**.

The extension sends only the selected search phrase and chosen filters to `https://www.qbreader.org/api/query`. It does not collect browsing history or require a QBReader account.

## API behavior

Queries default to exact phrase matching across both question text and answerlines, with up to 10 tossups and 10 bonuses displayed. QBReader currently limits its API to 20 requests per second.
