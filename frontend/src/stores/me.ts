import { defineStore } from 'pinia'

import { getMe } from '@/api/me'
import type { UserMe } from '@/api/schema'

type MeState = {
  me: UserMe | null
}

export const useMeStore = defineStore('me', {
  state: (): MeState => ({
    me: null,
  }),
  actions: {
    clear() {
      this.me = null
    },
    async load() {
      this.me = await getMe()
    },
  },
})
