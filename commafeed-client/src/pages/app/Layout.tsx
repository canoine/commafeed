import { Trans } from "@lingui/macro"
import { ActionIcon, AppShell, Box, Center, Group, ScrollArea, Title, useMantineTheme } from "@mantine/core"
import { Constants } from "app/constants"
import { redirectToAdd, redirectToRootCategory } from "app/redirect/thunks"
import { useAppDispatch, useAppSelector } from "app/store"
import { setMobileMenuOpen, setSidebarWidth } from "app/tree/slice"
import { reloadTree } from "app/tree/thunks"
import { reloadProfile, reloadSettings, reloadTags } from "app/user/thunks"
import { ActionButton } from "components/ActionButton"
import { AnnouncementDialog } from "components/AnnouncementDialog"
import { Loader } from "components/Loader"
import { Logo } from "components/Logo"
import { OnDesktop } from "components/responsive/OnDesktop"
import { OnMobile } from "components/responsive/OnMobile"
import { useAppLoading } from "hooks/useAppLoading"
import { useWebSocket } from "hooks/useWebSocket"
import { LoadingPage } from "pages/LoadingPage"
import { type ReactNode, Suspense, useEffect } from "react"
import Draggable from "react-draggable"
import { TbMenu2, TbPlus, TbX } from "react-icons/tb"
import { Outlet } from "react-router-dom"

interface LayoutProps {
    sidebar: ReactNode
    sidebarWidth: number
    sidebarVisible: boolean
    header: ReactNode
}

function LogoAndTitle() {
    const dispatch = useAppDispatch()
    return (
        <Center inline onClick={async () => await dispatch(redirectToRootCategory())} style={{ cursor: "pointer" }}>
            <Logo size={24} />
            <Title order={3} pl="md">
                CommaFeed
            </Title>
        </Center>
    )
}

export default function Layout(props: LayoutProps) {
    const theme = useMantineTheme()
    const { loading } = useAppLoading()
    const mobileMenuOpen = useAppSelector(state => state.tree.mobileMenuOpen)
    const webSocketConnected = useAppSelector(state => state.server.webSocketConnected)
    const treeReloadInterval = useAppSelector(state => state.server.serverInfos?.treeReloadInterval)
    const dispatch = useAppDispatch()
    useWebSocket()

    useEffect(() => {
        // load initial data
        dispatch(reloadSettings())
        dispatch(reloadProfile())
        dispatch(reloadTree())
        dispatch(reloadTags())
    }, [dispatch])

    useEffect(() => {
        let timer: number | undefined

        if (!webSocketConnected && treeReloadInterval) {
            // reload tree periodically if not receiving websocket events
            timer = window.setInterval(async () => await dispatch(reloadTree()), treeReloadInterval)
        }

        return () => clearInterval(timer)
    }, [dispatch, webSocketConnected, treeReloadInterval])

    const burger = (
        <ActionButton
            label={mobileMenuOpen ? <Trans>Open menu</Trans> : <Trans>Close menu</Trans>}
            icon={mobileMenuOpen ? <TbX size={18} /> : <TbMenu2 size={18} />}
            onClick={() => dispatch(setMobileMenuOpen(!mobileMenuOpen))}
        ></ActionButton>
    )

    const addButton = (
        <ActionIcon
            color={theme.primaryColor}
            variant="subtle"
            onClick={async () => await dispatch(redirectToAdd())}
            aria-label="Subscribe"
        >
            <TbPlus size={18} />
        </ActionIcon>
    )

    if (loading) return <LoadingPage />
    return (
        <AppShell
            header={{ height: Constants.layout.headerHeight }}
            navbar={{
                width: props.sidebarWidth,
                breakpoint: Constants.layout.mobileBreakpoint,
                collapsed: { mobile: !mobileMenuOpen, desktop: !props.sidebarVisible },
            }}
            padding={{ base: 6, [Constants.layout.mobileBreakpointName]: "md" }}
        >
            <AppShell.Header id="header">
                <OnMobile>
                    {mobileMenuOpen && (
                        <Group justify="space-between" p="md">
                            <Box>{burger}</Box>
                            <Box>
                                <LogoAndTitle />
                            </Box>
                            <Box>{addButton}</Box>
                        </Group>
                    )}
                    {!mobileMenuOpen && (
                        <Group p="md">
                            <Box>{burger}</Box>
                            <Box style={{ flexGrow: 1 }}>{props.header}</Box>
                        </Group>
                    )}
                </OnMobile>
                <OnDesktop>
                    <Group p="md">
                        <Group justify="space-between" style={{ width: props.sidebarWidth - 16 }}>
                            <Box>
                                <LogoAndTitle />
                            </Box>
                            <Box>{addButton}</Box>
                        </Group>
                        <Box style={{ flexGrow: 1 }}>{props.header}</Box>
                    </Group>
                </OnDesktop>
            </AppShell.Header>
            <AppShell.Navbar id="sidebar" p="xs">
                <AppShell.Section grow component={ScrollArea} mx="-sm" px="sm">
                    <Box>{props.sidebar}</Box>
                </AppShell.Section>
            </AppShell.Navbar>
            <Draggable
                axis="x"
                defaultPosition={{
                    x: props.sidebarWidth,
                    y: Constants.layout.headerHeight,
                }}
                bounds={{
                    left: 120,
                    right: 1000,
                }}
                grid={[30, 30]}
                onDrag={(_e, data) => {
                    dispatch(setSidebarWidth(data.x))
                }}
            >
                <Box
                    style={{
                        position: "fixed",
                        height: "100%",
                        width: "10px",
                        cursor: "ew-resize",
                    }}
                ></Box>
            </Draggable>

            <AppShell.Main id="content">
                <Suspense fallback={<Loader />}>
                    <AnnouncementDialog />
                    <Outlet />
                </Suspense>
            </AppShell.Main>
        </AppShell>
    )
}
