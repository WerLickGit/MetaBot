package io.github.werlickgit;

public class NoteDraft {
    private String title;
    private String tag;
    private String text;


    public NoteDraft(String title, String tag, String text) {
        this.title = title;
        this.tag = tag;
        this.text = text;
    }

    public NoteDraft() {
    }

    public String getTitle() {
        return title;
    }

    public String getTag() {
        return tag;
    }

    public String getText() {
        return text;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return "NoteDraft{" +
                "title='" + title + '\'' +
                ", tag='" + tag + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
