// lib/api/event.ts
import { EventListResponse } from "@/types/event/event";

const mockEvents: EventListResponse[] = [
  {
    id: 1,
    title: "新人歓迎会",
    joinCode: "ABC123",
    status: "DRAFT",
    createdAt: "2026-08-01T09:00:00Z",
  },
  {
    id: 2,
    title: "技術勉強会 #12",
    joinCode: "XYZ789",
    status: "OPEN",
    createdAt: "2026-08-05T13:30:00Z",
  },
  {
    id: 3,
    title: "忘年会",
    joinCode: "QWE456",
    status: "CLOSED",
    createdAt: "2026-07-20T18:00:00Z",
  },
];

export  function getEvents() {
    return mockEvents;
  }