export interface Page<T> {
  content: T[];
  empty: boolean;
  first: boolean;
  last: boolean;
  number: number;
  numberOfElements: number;
  pageable: {
    offset: number;
    pageNumber: number;
    pageSize: number;
    paged: boolean;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    unpaged: boolean;
  };
  size: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  totalElements: number;
  totalPages: number;
}

export interface User {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  email: string;
  pfpUrl: string;
  role: 'ADMIN' | 'USER';
  createdAt: Date;
  enabled: boolean;
  blockType?: 'TEMPORARY' | 'PERMANENT';
  blockReason?: string;
  blockedAt?: Date;
}

export interface Chat {
  id: string;
  name?: string;
  imageUrl?: string;
  type: 'DIRECT' | 'GROUP';
  createdAt: Date;
  members: ChatMember[];
  unreadCount: number;
}

export interface ChatMember {
  memberId: string;
  userId: string;
  username: string;
  firstName: string;
  lastName: string;
  fullName: string;
  pfpUrl: string;
  role: 'ADMIN' | 'MEMBER';
}

export interface Message {
  id: string;
  chatId: string;
  sender: ChatMember;
  content: string;
  replyToMessageId?: string;
  forwardedFromMessageId?: string;
  createdAt: Date;
}

export interface MessageReceipt {
  messageId: string;
  recipientMemberId: string;
  recipientUsername: string;
  recipientPfpUrl: string;
  status: 'SENT' | 'DELIVERED' | 'READ';
  deliveredAt: Date;
  readAt: Date;
}

export type ChatEventType = 'MESSAGE_CREATED';

export interface ChatEvent {
  type: ChatEventType;
  chatId: string;
  message: Message;
  unreadCount: number;
}
