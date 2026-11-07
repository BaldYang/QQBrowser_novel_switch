package com.QQBrowsernovelswitch.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class NovelChapterParser {

    public static final class Chapter {

        public final String title;
        public final String author;
        public final String body;

        Chapter(String title, String author, String body) {
            this.title = title;
            this.author = author;
            this.body = body;
        }
    }

    private NovelChapterParser() {
    }

    public static Chapter parse(String json, String fallbackTitle) throws JSONException {
        JSONObject object = new JSONObject(json);
        String title = object.optString("serialName").trim();
        if (title.isEmpty()) {
            title = fallbackTitle;
        }
        String author = extractAuthor(object);
        JSONArray contentArray = object.optJSONArray("content");
        StringBuilder body = new StringBuilder();
        if (contentArray != null) {
            for (int i = 0; i < contentArray.length(); i++) {
                String paragraph = contentArray.optString(i);
                if (paragraph.isEmpty()) {
                    continue;
                }
                if (body.length() > 0) {
                    body.append('\n');
                }
                body.append(paragraph);
            }
        }
        return new Chapter(title, author, body.toString());
    }

    public static String extractAuthor(JSONObject object) {
        String author = object.optString("author").trim();
        if (author.isEmpty()) {
            JSONObject bookInfo = object.optJSONObject("bookInfo");
            if (bookInfo != null) {
                author = bookInfo.optString("author").trim();
            }
        }
        return author;
    }
}
