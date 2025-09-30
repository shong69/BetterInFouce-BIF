import { HiMicrophone } from "react-icons/hi";

export default function RecordButton({ isRecording = false, onClick }) {
  return (
    <button
      onClick={onClick}
      className={`mb-3 flex w-full items-center justify-center gap-2 rounded-4xl px-2 py-3 font-medium text-white shadow-xl transition-all ${
        isRecording
          ? "bg-red-500 hover:bg-red-600"
          : "bg-[#343434] hover:bg-gray-700"
      }`}
    >
      {isRecording ? (
        <>
          <div className="h-4 w-4 rounded-sm bg-white" />
          음성 녹음 중단하기
        </>
      ) : (
        <>
          <HiMicrophone className="mb-1" />
          음성으로 작성하기
        </>
      )}
    </button>
  );
}
