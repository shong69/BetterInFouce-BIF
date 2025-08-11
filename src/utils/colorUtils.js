const colorPalette = [
  {
    title: "text-red-500",
    tag: "bg-red-100 text-red-500",
  },
  {
    title: "text-blue-600",
    tag: "bg-blue-100 text-blue-600",
  },
  {
    title: "text-green-600",
    tag: "bg-green-100 text-green-600",
  },
  {
    title: "text-orange-600",
    tag: "bg-orange-100 text-orange-600",
  },
  {
    title: "text-yellow-600",
    tag: "bg-yellow-100 text-yellow-600",
  },
];

export function getRandomColor(title) {
  const hash = title.split("").reduce((a, b) => {
    a = (a << 5) - a + b.charCodeAt(0);
    return a & a;
  }, 0);
  const index = Math.abs(hash) % colorPalette.length;
  return colorPalette[index];
}
