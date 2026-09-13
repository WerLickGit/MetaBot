package io.github.werlickgit;

import java.util.Objects;

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
        return """
                <b>📎 Заметка</b>
                                
                <i>🏷️ Тег:</i> <code>%s</code>
                <i>📝 Название: %s</i>
                                
                <b>Содержание заметки:</b>
                                
                <blockquote>%s</blockquote> 
                """.formatted(tag, title, text);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NoteDraft noteDraft = (NoteDraft) o;
        return Objects.equals(title, noteDraft.title) && Objects.equals(tag, noteDraft.tag) && Objects.equals(text, noteDraft.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, tag, text);
    }
}
