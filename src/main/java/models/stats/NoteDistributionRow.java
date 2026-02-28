package models.stats;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class NoteDistributionRow {

    private final IntegerProperty note = new SimpleIntegerProperty();
    private final IntegerProperty nb = new SimpleIntegerProperty();

    public NoteDistributionRow(int note, int nb) {
        this.note.set(note);
        this.nb.set(nb);
    }

    public int getNote() { return note.get(); }
    public void setNote(int v) { note.set(v); }
    public IntegerProperty noteProperty() { return note; }

    public int getNb() { return nb.get(); }
    public void setNb(int v) { nb.set(v); }
    public IntegerProperty nbProperty() { return nb; }
}
