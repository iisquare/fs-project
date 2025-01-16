import { useUserStore } from "@/stores/user"

export default {
  permit: {
    mounted (el: any, binding: any, vnode: any) {
      const user = useUserStore()
      if (!user.hasPermit(binding.value)) {
        el.remove()
      }
    }
  }
}
