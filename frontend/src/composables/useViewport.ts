import { computed, onMounted, onUnmounted, ref } from 'vue'

export function useViewport() {
  const width = ref(window.innerWidth)

  const onResize = () => {
    width.value = window.innerWidth
  }

  onMounted(() => {
    window.addEventListener('resize', onResize, { passive: true })
  })

  onUnmounted(() => {
    window.removeEventListener('resize', onResize)
  })

  return {
    width,
    isMobile: computed(() => width.value < 960),
  }
}
