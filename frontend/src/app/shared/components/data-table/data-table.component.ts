import { NgTemplateOutlet } from '@angular/common';
import { Component, input, TemplateRef } from '@angular/core';

export interface TableColumn<T> {
  key: keyof T;
  label: string;
}

@Component({
  selector: 'data-table',
  templateUrl: './data-table.component.html',
  imports: [NgTemplateOutlet],
})
export class DataTableComponent<T extends object> {
  columns = input.required<TableColumn<T>[]>();
  rows = input.required<T[]>();

  emptyMessage = input('/');

  actionTemplate = input<TemplateRef<{ $implicit: T }> | null>(null);
}
