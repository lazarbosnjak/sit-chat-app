import { Message, MessageReactionType } from '@shared/types/api.types';

export interface GroupSettingsModel {
  name: string;
  description: string;
  imageUrl: string;
}

export interface AddableGroupMember {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  fullName: string;
  phoneNumber?: string | null;
  pfpUrl: string;
  lastActiveAt?: Date | string | null;
}

export interface ReactionOption {
  type: MessageReactionType;
  emoji: string;
  label: string;
}

export interface MessageReactionRequest {
  message: Message;
  type: MessageReactionType;
}
