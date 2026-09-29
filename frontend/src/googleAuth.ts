export function loadGoogleScript(): Promise<void> {
  return new Promise((resolve) => {
    if (window.google?.accounts?.id) {
      resolve()
      return
    }
    const check = () => {
      if (window.google?.accounts?.id) {
        resolve()
      } else {
        setTimeout(check, 100)
      }
    }
    check()
  })
}
