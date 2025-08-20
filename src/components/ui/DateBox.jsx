import { getCurrentDate } from "@utils/dateUtils";

export default function DateBox() {
  return (
    <div className="flex items-center justify-between px-3 py-3">
      <div className="text-black-600 text-sm font-medium tracking-wide">
        {getCurrentDate()}
      </div>
    </div>
  );
}
