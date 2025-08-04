import { getCurrentDate } from "../../utils/dateUtils";

export default function DateBox() {
  return (
    <div className="flex items-center justify-between px-5 py-5">
      <div className="text-black-600 text-[13px] font-medium tracking-wide">
        {getCurrentDate()}
      </div>
    </div>
  );
}
