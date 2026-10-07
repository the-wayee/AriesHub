/** 保留完整读屏文案；逐字动画由所在区域的 GSAP 时间线统一调度。 */
export function AnimatedCaption({ text }: { text: string }) {
  return (
    <p className="animated-caption" aria-label={text}>
      <span aria-hidden="true">
        {Array.from(text).map((character, index) => (
          <span className="animated-caption-character" key={index}>
            {character === " " ? "\u00a0" : character}
          </span>
        ))}
      </span>
    </p>
  );
}
