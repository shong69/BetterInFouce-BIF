export default function ProgressBar({
  variant = "step",
  label = "진행도",
  progress = 0,
  currentStep = 0,
  totalSteps = 0,
  color = "bg-secondary",
  size = "md",
}) {
  const isPercentage = variant === "percentage";
  const calculatedProgress = isPercentage
    ? progress
    : totalSteps > 0
      ? (currentStep / totalSteps) * 100
      : 0;

  const sizeClasses = {
    sm: "h-1 gap-1 text-xs",
    md: "h-2 gap-2 text-sm",
    lg: "h-3 gap-3 text-base",
  };

  return (
    <div className={`flex flex-col ${sizeClasses[size]}`}>
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-gray-600">{label}</span>
        <span className="text-sm font-medium text-gray-600">
          {isPercentage
            ? `${Math.round(progress)}%`
            : `${currentStep}/${totalSteps}`}
        </span>
      </div>
      <div
        className={`bg-secondary/20 w-full rounded-full ${sizeClasses[size].split(" ")[0]}`}
      >
        <div
          className={`rounded-full transition-all duration-300 ${color} ${sizeClasses[size].split(" ")[0]}`}
          style={{ width: `${calculatedProgress}%` }}
        />
      </div>
    </div>
  );
}
