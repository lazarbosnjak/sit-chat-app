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
  lastActiveAt?: Date | null;
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
  deliveryStatus: MessageDeliveryStatus;
}

export type MessageDeliveryStatus = 'SENT' | 'DELIVERED' | 'READ';

export interface MessageReceipt {
  messageId: string;
  recipientMemberId: string;
  recipientUsername: string;
  recipientPfpUrl: string;
  status: MessageDeliveryStatus;
  deliveredAt: Date;
  readAt: Date;
}

export interface MessageStatus {
  messageId: string;
  status: MessageDeliveryStatus;
}

export type ChatEventType = 'MESSAGE_CREATED' | 'MESSAGE_STATUSES_UPDATED';

export interface ChatEvent {
  type: ChatEventType;
  chatId: string;
  message?: Message;
  unreadCount: number;
  messageStatuses?: MessageStatus[];
}

export type AnalyticsGranularity = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

export interface AnalyticsQuery {
  from: string;
  to: string;
  granularity: AnalyticsGranularity;
  topLimit: number;
}

export interface AnalyticsSeriesPoint {
  bucketStart: string;
  bucketEnd: string;
  registeredUsers: number;
  activeUsers: number;
  exchangedMessages: number;
  createdGroups: number;
}

export interface AnalyticsTopUser {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  pfpUrl: string;
  messageCount: number;
}

export interface AnalyticsTopGroup {
  id: string;
  name?: string | null;
  imageUrl?: string | null;
  messageCount: number;
}

export interface SystemAnalytics {
  from: string;
  to: string;
  granularity: AnalyticsGranularity;
  totalRegisteredUsers: number;
  totalActiveUsers: number;
  totalExchangedMessages: number;
  totalCreatedGroups: number;
  series: AnalyticsSeriesPoint[];
  topUsers: AnalyticsTopUser[];
  topGroups: AnalyticsTopGroup[];
}
