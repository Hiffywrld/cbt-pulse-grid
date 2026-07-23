import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { AppRoutes } from './app/app-routes'
import { ConnectivityBanner } from './components/feedback/connectivity-banner'
import { AuthProvider } from './features/auth/auth-provider'
import { ThemeProvider } from './features/theme/theme-provider'

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false }, mutations: { retry: false } } })

const App = () => <ThemeProvider><QueryClientProvider client={queryClient}><BrowserRouter><AuthProvider><ConnectivityBanner /><AppRoutes /></AuthProvider></BrowserRouter></QueryClientProvider></ThemeProvider>

export default App
