export default function Modal({
  isOpen,
  onClose,
  children,
  primaryButtonText,
  secondaryButtonText,
  onPrimaryClick,
  onSecondaryClick,
}) {
  if (!isOpen) return null;

  const handleBackgroundClick = (e) => {
    if (e.target === e.currentTarget) {
      onClose();
    }
  };

  const handlePrimaryClick = () => {
    if (onPrimaryClick) {
      onPrimaryClick();
    }
    onClose();
  };

  const handleSecondaryClick = () => {
    if (onSecondaryClick) {
      onSecondaryClick();
    }
    onClose();
  };

  const hasButtons = primaryButtonText || secondaryButtonText;

  return (
    <button
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70"
      onClick={handleBackgroundClick}
    >
      <div className="mx-4 w-full max-w-md rounded-lg bg-white p-6 shadow-xl">
        <div className="mb-6">{children}</div>

        {hasButtons && (
          <div className="flex justify-evenly space-x-15">
            {primaryButtonText && (
              <button
                onClick={handlePrimaryClick}
                className="bg-secondary hover:bg-primary ml-5 flex-1 rounded-3xl py-2 font-medium text-white transition-all"
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
    </button>
  );
}
