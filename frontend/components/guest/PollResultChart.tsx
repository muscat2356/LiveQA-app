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

export function PollResultChart({
    poll,
}:{
    poll: PollDto;
}) {

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
              <ChartTooltip 
                    content={
                        <ChartTooltipContent 
                            formatter={(value, _name, item) =>
                                `${value}票 (${item.payload.percent}%)`
                            }
                        />
                    } 
                />
              <Bar dataKey="count" fill="var(--color-count)" radius={4} />
          </BarChart>
      </ChartContainer>
    );
}