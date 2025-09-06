import { useEffect } from "react";

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
  useEffect(() => {
    if (isOpen) {
      const originalStyle = window.getComputedStyle(document.body).overflow;
      document.body.style.overflow = "hidden";
      document.body.style.position = "fixed";
      document.body.style.width = "100%";
      document.body.style.top = `-${window.scrollY}px`;

      return () => {
        document.body.style.overflow = originalStyle;
        document.body.style.position = "";
        document.body.style.width = "";
        document.body.style.top = "";
        window.scrollTo(0, parseInt(document.body.style.top || "0") * -1);
      };
    }
  }, [isOpen]);

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
      <div className="mx-4 max-h-[90vh] w-full max-w-sm overflow-y-auto rounded-lg bg-white p-4 shadow-xl sm:mx-10 sm:max-w-md sm:p-6">
        <div className="mb-4 sm:mb-6">{children}</div>

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
