export default function Modal({
  isOpen,
  onClose,
  children,
  icon,
  iconBgColor = "bg-gray-200", // ✅ 기본 배경색 추가
  primaryButtonText,
  secondaryButtonText,
  primaryButtonColor = "bg-secondary",
  secondaryButtonColor = "bg-gray-400",
  onPrimaryClick,
  onSecondaryClick,
}) {
  if (!isOpen) return null;

  function handleBackgroundClick(e) {
    if (e.target === e.currentTarget) {
      onClose();
    }
  }

  function handlePrimaryClick() {
    if (onPrimaryClick) onPrimaryClick();
    onClose();
  }

  function handleSecondaryClick() {
    if (onSecondaryClick) onSecondaryClick();
    onClose();
  }

  const hasButtons = primaryButtonText || secondaryButtonText;

  return (
    <div
      role="button"
      tabIndex={0}
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70"
      onClick={handleBackgroundClick}
      onKeyDown={(e) => {
        if (e.key === "Enter" || e.key === " ") {
          handleBackgroundClick(e);
        }
      }}
    >
      <div className="mx-16 w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
        {icon && (
          <div className="mb-4 flex justify-center">
            <div
              className={`flex h-14 w-14 items-center justify-center rounded-full ${iconBgColor}`}
            >
              {icon}
            </div>
          </div>
        )}

        <div className="mb-6">{children}</div>

        {hasButtons && (
          <div className="flex justify-center space-x-10">
            {primaryButtonText && (
              <button
                onClick={handlePrimaryClick}
                className={`${primaryButtonColor} min-w-[70px] rounded-3xl px-4 py-1.5 text-sm font-medium text-white transition-all hover:opacity-90`}
              >
                {primaryButtonText}
              </button>
            )}

            {secondaryButtonText && (
              <button
                onClick={handleSecondaryClick}
                className={`${secondaryButtonColor} min-w-[70px] rounded-3xl px-4 py-1.5 text-sm font-medium text-white transition-all hover:opacity-90`}
              >
                {secondaryButtonText}
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
