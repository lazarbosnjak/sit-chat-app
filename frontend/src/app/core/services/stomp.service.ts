import { inject, Injectable, signal } from '@angular/core';
import { AuthService } from '@core/services/auth.service';
import { environment as env } from '@environments/environment';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

@Injectable({
  providedIn: 'root',
})
export class StompService {
  private readonly authService = inject(AuthService);

  connected = signal(false);
  private onConnectCallback?: () => void;

  private readonly client = new Client({
    brokerURL: env.wsUrl,

    beforeConnect: async () => {
      this.client.connectHeaders = {
        Authorization: `Bearer ${this.authService.getToken()}`,
      };
    },

    debug: (msg) => console.log(`[STOMP]`, msg),

    onConnect: () => {
      this.connected.set(true);
      this.onConnectCallback?.();
    },

    onDisconnect: () => {
      this.connected.set(false);
    },

    onWebSocketClose: () => {
      this.connected.set(false);
    },

    onStompError: (frame) => {
      console.error(`STOMP Error:`, frame.headers['message'], frame.body);
    },
  });

  connect(onConnect?: () => void) {
    if (this.client.connected) {
      onConnect?.();
      return;
    }

    if (onConnect) {
      this.onConnectCallback = onConnect;
    }

    if (this.client.active) {
      return;
    }

    this.client.activate();
  }

  disconnect() {
    this.client.deactivate();
  }

  publish(destination: string, body: string) {
    if (!this.client.connected) {
      console.warn('STOMP client not connected');
      return;
    }

    this.client.publish({
      destination: destination,
      body: body,
    });
  }

  subscribe(destination: string, callback: (message: IMessage) => void): StompSubscription | null {
    if (!this.client.connected) {
      console.warn('STOMP client not connected');
      return null;
    }

    return this.client.subscribe(destination, callback);
  }
}
