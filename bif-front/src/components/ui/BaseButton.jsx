export default function BaseButton({
  title,
  children,
  onClick,
  className = "",
  variant = "primary",
}) {
  const buttonText = title || children;

  const variantStyles = {
    primary: "bg-primary hover:bg-[#0A7B06] text-white",
    secondary: "bg-gray-300 hover:bg-gray-400 text-gray-700",
    danger: "bg-red-500 hover:bg-red-600 text-white",
    success: "bg-green-500 hover:bg-green-600 text-white",
    warning: "bg-yellow-500 hover:bg-yellow-600 text-white",
  };

  return (
    <button
      onClick={onClick}
      className={`w-full rounded-full px-4 py-2 font-medium transition-colors ${variantStyles[variant]} ${className}`}
    >
      {buttonText}
    </button>
  );
}
