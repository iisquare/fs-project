<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps({
  widget: { type: Object, required: true },
})

const styleVars = computed(() => ({
  '--decoration-accent': props.widget.style.borderAccent,
  '--decoration-glow': `${Math.max(0, props.widget.style.glow)}px`,
  '--decoration-color': props.widget.style.color,
}))
const animated = computed(() => props.widget.style.animation !== false)
const pattern = computed(() => props.widget.style.decorationPattern || 'hud')
</script>

<template>
  <div
    class="dashboard-decoration"
    :class="[
      `is-${widget.type}`,
      `pattern-${pattern}`,
      widget.style.borderVariant ? `variant-${widget.style.borderVariant}` : '',
      { 'is-animated': animated },
    ]"
    :style="styleVars">
    <template v-if="widget.type === 'Border'">
      <span class="frame-line frame-line--top"></span>
      <span class="frame-line frame-line--right"></span>
      <span class="frame-line frame-line--bottom"></span>
      <span class="frame-line frame-line--left"></span>
      <i class="frame-corner frame-corner--lt"></i>
      <i class="frame-corner frame-corner--rt"></i>
      <i class="frame-corner frame-corner--lb"></i>
      <i class="frame-corner frame-corner--rb"></i>
      <span class="frame-node frame-node--top"></span>
      <span class="frame-node frame-node--bottom"></span>
    </template>

    <template v-else-if="widget.type === 'DecorationHeader'">
      <span class="header-wing header-wing--left"></span>
      <span class="header-wing header-wing--right"></span>
      <span class="header-line header-line--left"></span>
      <span class="header-line header-line--right"></span>
      <div class="header-core">
        <i class="header-index">01</i>
        <strong>{{ widget.content || '数据可视化' }}</strong>
        <span class="header-subline"></span>
      </div>
      <span class="header-star header-star--left"></span>
      <span class="header-star header-star--right"></span>
    </template>

    <template v-else-if="widget.type === 'DecorationScan'">
      <div class="scan-grid"></div>
      <div class="scan-radar scan-radar--one"></div>
      <div class="scan-radar scan-radar--two"></div>
      <span class="scan-beam"></span>
      <span class="scan-edge scan-edge--lt"></span>
      <span class="scan-edge scan-edge--rt"></span>
      <span class="scan-edge scan-edge--lb"></span>
      <span class="scan-edge scan-edge--rb"></span>
      <span class="scan-dot scan-dot--one"></span>
      <span class="scan-dot scan-dot--two"></span>
      <span class="scan-dot scan-dot--three"></span>
    </template>

    <template v-else-if="widget.type === 'DecorationGrid'">
      <div class="perspective-grid"></div>
      <div class="perspective-haze"></div>
      <span class="grid-horizon"></span>
      <span class="grid-arc grid-arc--one"></span>
      <span class="grid-arc grid-arc--two"></span>
    </template>

    <template v-else-if="widget.type === 'DecorationDivider'">
      <span class="divider-beam divider-beam--left"></span>
      <span class="divider-beam divider-beam--right"></span>
      <span class="divider-diamond"></span>
      <span class="divider-core"></span>
      <span class="divider-dot divider-dot--left"></span>
      <span class="divider-dot divider-dot--right"></span>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.dashboard-decoration {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 16px;
  overflow: hidden;
  color: var(--decoration-color);
  pointer-events: none;
  --decoration-accent: #19d3ff;
  --decoration-glow: 16px;
}

.frame-line {
  position: absolute;
  opacity: 0.72;
  background: linear-gradient(90deg, transparent, var(--decoration-accent), transparent);
  &--top,
  &--bottom {
    left: 9%;
    right: 9%;
    height: 1px;
  }
  &--top {
    top: 0;
  }
  &--bottom {
    bottom: 0;
  }
  &--left,
  &--right {
    top: 12%;
    bottom: 12%;
    width: 1px;
    background: linear-gradient(180deg, transparent, var(--decoration-accent), transparent);
  }
  &--left {
    left: 0;
  }
  &--right {
    right: 0;
  }
}

.frame-corner {
  position: absolute;
  width: 24px;
  height: 24px;
  border-color: var(--decoration-accent);
  border-style: solid;
  filter: drop-shadow(0 0 5px var(--decoration-accent));
  &--lt {
    left: 0;
    top: 0;
    border-width: 2px 0 0 2px;
  }
  &--rt {
    right: 0;
    top: 0;
    border-width: 2px 2px 0 0;
  }
  &--lb {
    left: 0;
    bottom: 0;
    border-width: 0 0 2px 2px;
  }
  &--rb {
    right: 0;
    bottom: 0;
    border-width: 0 2px 2px 0;
  }
}

.frame-node {
  position: absolute;
  left: 50%;
  width: 7px;
  height: 7px;
  border: 1px solid var(--decoration-accent);
  background: #06172c;
  transform: translateX(-50%) rotate(45deg);
  &--top {
    top: -4px;
  }
  &--bottom {
    bottom: -4px;
  }
}

.variant-simple {
  border: 1px solid color-mix(in srgb, var(--decoration-accent) 55%, transparent);
  border-radius: inherit;
  .frame-corner,
  .frame-node {
    display: none;
  }
}

.variant-corner .frame-line {
  opacity: 0.2;
}

.is-DecorationHeader {
  display: flex;
  align-items: center;
  justify-content: center;
}
.header-core {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-width: 34%;
  height: 100%;
  padding: 0 34px;
  background:
    linear-gradient(90deg, transparent, color-mix(in srgb, var(--decoration-accent) 15%, transparent) 16%, rgba(4, 28, 58, 0.82) 42%, rgba(4, 28, 58, 0.82) 58%, color-mix(in srgb, var(--decoration-accent) 15%, transparent) 84%, transparent);
  clip-path: polygon(10% 0, 90% 0, 100% 50%, 90% 100%, 10% 100%, 0 50%);
  strong {
    max-width: 70%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: inherit;
    font-family: inherit;
    letter-spacing: 4px;
    text-shadow: 0 0 calc(var(--decoration-glow) * 0.65) var(--decoration-accent);
  }
}
.header-index {
  color: color-mix(in srgb, var(--decoration-accent) 75%, white);
  font-size: 0.42em;
  font-style: normal;
  letter-spacing: 1px;
  opacity: 0.75;
}
.header-subline {
  position: absolute;
  left: 18%;
  right: 18%;
  bottom: 11%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--decoration-accent), transparent);
  box-shadow: 0 0 var(--decoration-glow) var(--decoration-accent);
}
.header-wing {
  position: absolute;
  top: 50%;
  width: 22%;
  height: 26%;
  border-top: 1px solid color-mix(in srgb, var(--decoration-accent) 58%, transparent);
  background: linear-gradient(180deg, color-mix(in srgb, var(--decoration-accent) 11%, transparent), transparent);
  &--left {
    left: 0;
    border-left: 2px solid var(--decoration-accent);
    clip-path: polygon(0 0, 100% 0, 86% 100%, 0 100%);
  }
  &--right {
    right: 0;
    border-right: 2px solid var(--decoration-accent);
    clip-path: polygon(0 0, 100% 0, 100% 100%, 14% 100%);
  }
}
.header-line {
  position: absolute;
  bottom: 7%;
  width: 16%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--decoration-accent));
  &--right {
    right: 0;
    transform: rotate(180deg);
  }
}
.header-star {
  position: absolute;
  top: 50%;
  width: 7px;
  height: 7px;
  border: 1px solid var(--decoration-accent);
  transform: translateY(-50%) rotate(45deg);
  &--left {
    left: 25%;
  }
  &--right {
    right: 25%;
  }
}

.is-DecorationScan {
  border: 1px solid color-mix(in srgb, var(--decoration-accent) 24%, transparent);
  border-radius: inherit;
  background:
    radial-gradient(circle at 50% 52%, color-mix(in srgb, var(--decoration-accent) 12%, transparent), transparent 42%),
    linear-gradient(180deg, rgba(4, 20, 42, 0.55), rgba(2, 13, 30, 0.18));
}
.scan-grid {
  position: absolute;
  inset: 12%;
  opacity: 0.24;
  background-image:
    linear-gradient(color-mix(in srgb, var(--decoration-accent) 28%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--decoration-accent) 28%, transparent) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: radial-gradient(circle, #000 0%, transparent 72%);
}
.scan-radar {
  position: absolute;
  left: 50%;
  top: 52%;
  border: 1px solid color-mix(in srgb, var(--decoration-accent) 38%, transparent);
  border-radius: 50%;
  transform: translate(-50%, -50%);
  &--one {
    width: 48%;
    aspect-ratio: 1;
  }
  &--two {
    width: 72%;
    aspect-ratio: 1;
    border-style: dashed;
    opacity: 0.5;
  }
}
.scan-beam {
  position: absolute;
  top: 0;
  bottom: 0;
  left: -25%;
  width: 22%;
  background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--decoration-accent) 45%, transparent), transparent);
  filter: blur(2px);
  transform: skewX(-16deg);
}
.is-animated .scan-beam {
  animation: dashboard-scan 4.2s linear infinite;
}
.scan-edge {
  position: absolute;
  width: 18px;
  height: 18px;
  border-color: var(--decoration-accent);
  border-style: solid;
  filter: drop-shadow(0 0 4px var(--decoration-accent));
  &--lt {
    left: 0;
    top: 0;
    border-width: 2px 0 0 2px;
  }
  &--rt {
    right: 0;
    top: 0;
    border-width: 2px 2px 0 0;
  }
  &--lb {
    left: 0;
    bottom: 0;
    border-width: 0 0 2px 2px;
  }
  &--rb {
    right: 0;
    bottom: 0;
    border-width: 0 2px 2px 0;
  }
}
.scan-dot {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--decoration-accent);
  box-shadow: 0 0 8px var(--decoration-accent);
  &--one {
    left: 24%;
    top: 35%;
  }
  &--two {
    right: 20%;
    top: 62%;
  }
  &--three {
    left: 56%;
    bottom: 18%;
  }
}

.is-DecorationGrid {
  background: linear-gradient(180deg, transparent, rgba(4, 28, 54, 0.4));
}
.perspective-grid {
  position: absolute;
  left: -30%;
  right: -30%;
  bottom: -20%;
  height: 88%;
  background-image:
    linear-gradient(color-mix(in srgb, var(--decoration-accent) 38%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--decoration-accent) 38%, transparent) 1px, transparent 1px);
  background-size: 46px 30px;
  opacity: 0.3;
  transform: perspective(320px) rotateX(62deg);
  transform-origin: center bottom;
  mask-image: linear-gradient(180deg, transparent, #000 36%, #000);
}
.is-animated .perspective-grid {
  animation: dashboard-grid-flow 5s linear infinite;
}
.perspective-haze {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 50% 72%, color-mix(in srgb, var(--decoration-accent) 18%, transparent), transparent 55%);
}
.grid-horizon {
  position: absolute;
  left: 8%;
  right: 8%;
  top: 58%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--decoration-accent), transparent);
  box-shadow: 0 0 var(--decoration-glow) var(--decoration-accent);
}
.grid-arc {
  position: absolute;
  left: 50%;
  top: 58%;
  border: 1px solid color-mix(in srgb, var(--decoration-accent) 30%, transparent);
  border-radius: 50% 50% 0 0;
  transform: translate(-50%, -50%);
  &--one {
    width: 34%;
    height: 34%;
  }
  &--two {
    width: 56%;
    height: 56%;
    opacity: 0.55;
  }
}

.is-DecorationDivider {
  display: flex;
  align-items: center;
  justify-content: center;
}
.divider-beam {
  width: 38%;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--decoration-accent));
  box-shadow: 0 0 var(--decoration-glow) color-mix(in srgb, var(--decoration-accent) 55%, transparent);
  &--right {
    transform: rotate(180deg);
  }
}
.divider-diamond {
  z-index: 1;
  width: 12px;
  height: 12px;
  margin: 0 12px;
  border: 2px solid var(--decoration-accent);
  background: rgba(3, 18, 36, 0.9);
  transform: rotate(45deg);
  box-shadow: 0 0 10px var(--decoration-accent);
}
.divider-core {
  position: absolute;
  width: 42%;
  height: 1px;
  background: linear-gradient(90deg, transparent, var(--decoration-accent), transparent);
  filter: brightness(1.3);
}
.divider-dot {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--decoration-accent);
  &--left {
    left: 8%;
  }
  &--right {
    right: 8%;
  }
}

.pattern-grid .scan-grid {
  inset: 0;
  mask-image: none;
}
.pattern-wave .scan-radar {
  border-color: transparent;
  border-top-color: color-mix(in srgb, var(--decoration-accent) 55%, transparent);
}
.pattern-radar .scan-beam {
  left: 49%;
  top: 50%;
  width: 42%;
  height: 1px;
  transform-origin: left center;
  animation-name: dashboard-radar;
}

@keyframes dashboard-scan {
  from { transform: translateX(0) skewX(-16deg); }
  to { transform: translateX(600%) skewX(-16deg); }
}
@keyframes dashboard-grid-flow {
  from { background-position: 0 0, 0 0; }
  to { background-position: 0 30px, 0 0; }
}
@keyframes dashboard-radar {
  to { transform: rotate(360deg); }
}

@media (prefers-reduced-motion: reduce) {
  .is-animated .scan-beam,
  .is-animated .perspective-grid {
    animation: none;
  }
}
</style>
