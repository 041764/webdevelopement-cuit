import type { GlobalThemeOverrides } from 'naive-ui'

export const themeOverrides: GlobalThemeOverrides = {
  common: {
    fontFamily: "'Source Sans 3', 'Noto Sans SC', 'Noto Sans', 'PingFang SC', 'Microsoft YaHei', sans-serif",

    primaryColor: '#0ea5b7',
    primaryColorHover: '#12b6c8',
    primaryColorPressed: '#0b8ea0',

    bodyColor: '#f7f4ee',
    cardColor: '#ffffff',
    modalColor: '#ffffff',

    borderRadius: '18px',

    textColorBase: '#161a1f',
    textColor2: '#5e6a78',
  },
  Layout: {
    color: 'transparent',
    siderColor: '#fbfaf6',
    headerColor: 'transparent',
  },
  Card: {
    borderRadius: '24px',
    color: '#ffffff',
  },
  Button: {
    borderRadius: '18px',
    heightMedium: '36px',
  },
  Input: {
    borderRadius: '18px',
    heightMedium: '36px',
  },
  Menu: {
    itemHeight: '42px',
    borderRadius: '18px',
  },
}
