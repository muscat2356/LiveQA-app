"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { fetchJoinEvent } from "@/lib/api/server/eventApi";

export type EnterState = {
  error?: string;
};

export async function enterRoom(
  _prev: EnterState,
  formData: FormData
): Promise<EnterState> {
  const joinCode = String(formData.get("joinCode") ?? "").trim();
  const nickname = String(formData.get("nickname") ?? "").trim().slice(0, 20);

  if (!joinCode) return { error: "不正なリクエストです" };

  const event = await fetchJoinEvent(joinCode);
  if (!event) return { error: "該当するルームがありません" };

  if (event.status !== "OPEN") {
    return { error: "このルームは現在受け付けていません" };
  }

  const store = await cookies();

  if (nickname) {
    store.set("qa_nickname", nickname, {
      path: "/",
      maxAge: 60 * 60 * 24 * 30,
      sameSite: "lax",
      httpOnly: true,
    });
  } else {
    store.delete("qa_nickname");
  }

  redirect(`/join/${joinCode}/qa`);
}