import { useLoadingStore } from "@stores/loadingStore";

export default function LoadingSpinner() {
  const { isLoading, message } = useLoadingStore();

  if (!isLoading) return null;

  return (
    <div className="fixed inset-0 z-[9999] flex items-center justify-center bg-black/70 backdrop-blur-sm">
      <div className="rounded-lg bg-white p-6 shadow-xl">
        <div className="flex flex-col items-center space-y-4">
          <div className="border-primary h-12 w-12 animate-spin rounded-full border-4 border-solid border-t-transparent" />
          <p className="text-sm font-medium text-gray-700">{message}</p>
        </div>
      </div>
    </div>
  );
}
