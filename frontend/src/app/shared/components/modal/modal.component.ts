import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-modal',
  templateUrl: './modal.component.html',
})
export class ModalComponent {
  title = input.required<string>();
  wide = input(false);

  close = output<void>();

  closeModal() {
    this.close.emit();
  }
}
