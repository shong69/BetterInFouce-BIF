export default function Modal({
  isOpen,
  onClose,
  children,
  primaryButtonText,
  secondaryButtonText,
  primaryButtonColor = "bg-secondary",
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
    if (onPrimaryClick) {
      onPrimaryClick();
    }
    onClose();
  }

  function handleSecondaryClick() {
    if (onSecondaryClick) {
      onSecondaryClick();
    }
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
      <div className="mx-10 w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
        <div className="mb-6">{children}</div>

        {hasButtons && (
          <div className="flex justify-evenly space-x-15">
            {primaryButtonText && (
              <button
                onClick={handlePrimaryClick}
                className={`${primaryButtonColor} hover:bg-primary ml-5 flex-1 rounded-3xl py-2 font-medium text-white transition-all`}
              >
                {primaryButtonText}
              </button>
            )}

            {secondaryButtonText && (
              <button
                onClick={handleSecondaryClick}
                className="bg-gray-medium mr-5 flex-1 rounded-3xl py-2 font-medium text-white transition-all hover:bg-gray-400"
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
