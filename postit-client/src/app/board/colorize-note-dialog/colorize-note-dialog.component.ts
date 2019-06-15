import { Component, OnInit, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { PostitService } from 'src/app/shared/service/postit/postit.service';
import { PostitNote } from 'src/app/shared/model/postit/postit-note';

export interface ColorizeNoteDialogData {
  noteId: number;
}

@Component({
  selector: 'app-colorize-note-dialog',
  templateUrl: './colorize-note-dialog.component.html',
  styleUrls: ['./colorize-note-dialog.component.scss']
})
export class ColorizeNoteDialogComponent implements OnInit {

  public constructor(
    private dialogRef: MatDialogRef<ColorizeNoteDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ColorizeNoteDialogData,
    private postitService: PostitService
  ) { }

  public ngOnInit() {
  }

  public chooseColor(color: string) {
    const saveNote = new PostitNote();
    saveNote.id = this.data.noteId;
    saveNote.color = color;

    this.postitService.updateNote(saveNote).then(updatedNote => {
      this.dialogRef.close(updatedNote);
    });
  }

}
