import api from '@renderer/core/Api'

export default {
  check(body: {
    currentVersion: string
    platform: string
    architecture: string
    clientId: string
    channel: string
  }) {
    return api.post(
      '/agent/clientUpdate/check',
      body,
      { warning: false, error: false },
      { timeout: 15000 }
    )
  },
  report(body: {
    clientId: string
    platform: string
    architecture: string
    channel: string
    fromVersion: string
    targetVersion: string
    event: string
    message?: string
  }) {
    return api.post(
      '/agent/clientUpdate/report',
      body,
      { warning: false, error: false },
      { timeout: 10000 }
    )
  },
  delta(id: number) {
    return api.get(
      '/agent/clientUpdate/delta',
      { id },
      { warning: false, error: false },
      { timeout: 15000 }
    )
  }
}
