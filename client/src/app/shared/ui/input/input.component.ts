import { Component, input, output } from '@angular/core';

export type InputType = 'text' | 'email' | 'password' | 'number';

@Component({
  selector: 'app-input',
  templateUrl: './input.component.html',
  styleUrl: './input.component.scss',
})
export class InputComponent {
  readonly type = input<InputType>('text');
  readonly value = input<string>('');
  readonly placeholder = input<string>('');
  readonly autocomplete = input<string>('off');
  readonly disabled = input(false);
  readonly id = input<string>('');
  readonly invalid = input(false);

  readonly valueChange = output<string>();

  onInput(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.valueChange.emit(target.value);
  }
}
