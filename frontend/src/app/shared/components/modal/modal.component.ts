import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
})
export class ModalComponent {
  title = input.required<string>();

  close = output<void>();

  closeModal() {
    this.close.emit();
  }
}
