import { useEffect, useState } from 'react';

const CLOCK_UPDATE_INTERVAL_MS = 1000;

export function useComputerTime() {
  const [computerTime, setComputerTime] = useState(() => formatComputerTime(new Date()));

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      setComputerTime(formatComputerTime(new Date()));
    }, CLOCK_UPDATE_INTERVAL_MS);

    return () => window.clearInterval(intervalId);
  }, []);

  return computerTime;
}

function formatComputerTime(date: Date) {
  const year = date.getFullYear();
  const month = padDatePart(date.getMonth() + 1);
  const day = padDatePart(date.getDate());
  const hours = padDatePart(date.getHours());
  const minutes = padDatePart(date.getMinutes());
  const seconds = padDatePart(date.getSeconds());

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

function padDatePart(value: number) {
  return String(value).padStart(2, '0');
}
