const colorPalette = [
  {
    title: "text-red-500",
    tag: "bg-red-100 text-red-500",
    button: "bg-red-500 text-red-100",
  },
  {
    title: "text-blue-600",
    tag: "bg-blue-100 text-blue-600",
    button: "bg-blue-600 text-blue-100",
  },
  {
    title: "text-green-600",
    tag: "bg-green-100 text-green-600",
    button: "bg-green-600 text-green-100",
  },
  {
    title: "text-orange-600",
    tag: "bg-orange-100 text-orange-600",
    button: "bg-orange-600 text-orange-100",
  },
  {
    title: "text-yellow-600",
    tag: "bg-yellow-100 text-yellow-600",
    button: "bg-yellow-600 text-yellow-100",
  },
];

export function getRandomColorByTitle(title, id = "") {
  if (!title || typeof title !== "string") {
    title = "default";
  }

  const seed = `${title}-${id}`;
  const hash = seed.split("").reduce((a, b) => {
    a = (a << 5) - a + b.charCodeAt(0);
    return a & a;
  }, 0);

  const index = Math.abs(hash) % colorPalette.length;
  return colorPalette[index];
}
