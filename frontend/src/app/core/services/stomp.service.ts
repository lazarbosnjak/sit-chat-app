import { Injectable, signal } from '@angular/core';
import { environment as env } from '@environments/environment';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

@Injectable({
  providedIn: 'root',
})
export class StompService {
  private connected = signal(false);
  private connectCallbacks: (() => void)[] = [];

  private token: string | null = null;

  private readonly client = new Client({
    brokerURL: env.wsUrl,

    beforeConnect: async () => {
      this.client.connectHeaders = this.token
        ? {
            Authorization: `Bearer ${this.token}`,
          }
        : {};
    },

    debug: (msg) => console.log(`[STOMP]`, msg),

    onConnect: () => {
      console.log('StompService onConnect fired');
      this.connected.set(true);
      const callbacks = [...this.connectCallbacks];
      this.connectCallbacks = [];

      callbacks.forEach((callback) => callback());
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

  connect(token: string | null, onConnect?: () => void): void {
    this.token = token;

    if (this.client.connected) {
      onConnect?.();
      return;
    }

    if (onConnect) {
      this.connectCallbacks.push(onConnect);
    }

    if (!this.client.active) {
      this.client.activate();
    }
  }

  async disconnect(): Promise<void> {
    this.connectCallbacks = [];
    this.token = null;

    if (this.client.active) {
      await this.client.deactivate();
    }

    this.connected.set(false);
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

  publishJson(destination: string, body: unknown) {
    if (!this.client.connected) {
      console.warn('STOMP client not connected');
      return;
    }

    this.client.publish({
      destination: destination,
      body: JSON.stringify(body),
      headers: {
        'content-type': 'application/json',
      },
    });
  }

  subscribe(destination: string, callback: (message: IMessage) => void): StompSubscription | null {
    if (!this.client.connected) {
      console.warn('STOMP client not connected');
      return null;
    }

    console.log('Subscribing to:', destination);

    return this.client.subscribe(destination, callback);
  }

  subscribeJson<T>(destination: string, callback: (body: T) => void): StompSubscription | null {
    return this.client.subscribe(destination, (message) => {
      callback(JSON.parse(message.body) as T);
    });
  }
}
