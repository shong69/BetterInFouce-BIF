import { IoCheckmarkCircle } from "react-icons/io5";

export default function CompletionMessage({ type, color }) {
  if (type === "checklist") {
    return (
      <div
        className={`mt-4 rounded-xl border border-green-200 bg-green-50 p-4 text-center ${color}`}
      >
        <div className="mb-2">
          <IoCheckmarkCircle className="mx-auto h-12 w-12" />
        </div>
        <p className="font-medium">모든 할 일을 완료했어요! 🎉</p>
        <p className="mt-1 text-sm">수고하셨습니다!</p>
      </div>
    );
  }

  if (type === "sequence") {
    return (
      <div className={`mt-4 rounded-xl border p-4 text-center ${color}`}>
        <div className="mb-2">
          <svg
            className="mx-auto h-12 w-12"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        </div>
        <p className="font-medium">모든 단계를 완료했습니다! 🎯</p>
        <p className="mt-1 text-sm">정말 멋지게 마무리하셨어요!</p>
      </div>
    );
  }

  return null;
}
