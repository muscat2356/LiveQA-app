"use client"

import { PollDto } from "@/types/poll/poll";
import { ChartConfig, ChartContainer, ChartTooltip, ChartTooltipContent } from "../ui/chart";
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts";

const chartConfig = {
  count:{
      label: "票数",
      color: "var(--chart-1)",
  },
} satisfies ChartConfig;

export function OrganizerChart({
    pending,
    error,
    poll
}:{
    pending: boolean;
    error: string;
    poll: PollDto | null;
}) {

    if (pending && !poll) return <p className="py-6 text-center text-sm text-muted-foreground">読み込み中・・・</p>;
    if (error && !poll)   return <p className="rounded-lg bg-destructive/10 px-4 py-3 text-sm text-destructive">{error}</p>;
    if (!poll)   return null;

    const total = poll.totalResponses;

    const data = poll.options.map((o) => ({
        label: o.label,
        count: o.count,
        percent: total > 0 ? Math.round(( o.count / total ) * 100 ): 0,
    }));

    return (
      <ChartContainer config={chartConfig} className="mb-4 min-h-[200px] w-full">
          <BarChart accessibilityLayer data={data} layout="vertical" margin={{ left: 0, right: 16 }}>
              <CartesianGrid horizontal={false} />
              <XAxis type="number" dataKey="count" hide />
              <YAxis
                  type="category"
                  dataKey="label"
                  tickLine={false}
                  axisLine={false}
                  width={120}
                  tickMargin={8}
                  tick={{ fontSize: 12 }}
              />
              <ChartTooltip content={<ChartTooltipContent />} />
              <Bar dataKey="count" fill="var(--color-count)" radius={4} />
          </BarChart>
      </ChartContainer>
    );
}