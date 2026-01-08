# Premium UI Design System

## Overview
A modern, trust-based design system inspired by PhonePe, Google Pay, and other leading Indian fintech apps.

## Design Principles
1. **Trust & Security**: Calming blue/teal colors convey reliability
2. **Elderly-Friendly**: Large text sizes (12sp-32sp), 48dp+ touch targets
3. **Accessibility**: High contrast ratios, clear visual hierarchy
4. **Role-Based**: Customer and Provider experiences
5. **Modern Premium**: Clean cards, soft shadows, smooth transitions

## Color Palette

### Primary Colors
- **Trust Blue**: `#0891B2` - Primary actions, headers, key elements
- **Primary Dark**: `#0E7490` - Status bar, pressed states
- **Primary Light**: `#06B6D4` - Highlights, accents
- **Primary Container**: `#CFFAFE` - Light backgrounds, chips

### Secondary Colors
- **Fresh Green**: `#10B981` - Success, positive actions
- **Secondary Dark**: `#059669` - Pressed green states
- **Secondary Light**: `#34D399` - Subtle green highlights

### Accent Colors
- **Soft Orange**: `#FB923C` - Warnings, attention areas
- **Accent Dark**: `#F97316` - Pressed orange states

### Status Colors
- **Error**: `#EF4444` (Red) - Errors, destructive actions
- **Warning**: `#F59E0B` (Amber) - Warnings, cautions
- **Success**: `#10B981` (Green) - Success states
- **Info**: `#3B82F6` (Blue) - Informational messages

### Surface & Background
- **Background**: `#F9FAFB` - App background (light)
- **Surface**: `#FFFFFF` - Cards, sheets (light)
- **Surface Variant**: `#F3F4F6` - Secondary surfaces
- **Border**: `#E5E7EB` - Dividers, outlines

### Text Colors
- **Primary**: `#111827` - Main text (90% opacity)
- **Secondary**: `#6B7280` - Supporting text (60% opacity)
- **Tertiary**: `#9CA3AF` - Hints, placeholders (38% opacity)

### Dark Mode Support
All colors have dark mode variants with proper contrast ratios.

## Typography Scale

### Headings
- **XXL**: 32sp - Hero text
- **XL**: 24sp - Screen titles
- **Large**: 20sp - Section headers
- **Medium**: 18sp - Card titles

### Body Text
- **Base**: 16sp - Default text
- **Small**: 14sp - Supporting text
- **XSmall**: 12sp - Captions, labels

### Font Families
- **Medium Weight**: Sans-serif-medium (headings, buttons)
- **Regular**: Sans-serif (body text)

## Spacing System (8dp Grid)
- **XS**: 4dp - Tight spacing
- **SM**: 8dp - Default spacing
- **MD**: 16dp - Card padding
- **LG**: 24dp - Section spacing
- **XL**: 32dp - Screen margins
- **XXL**: 40dp - Large gaps
- **XXXL**: 48dp - Hero spacing

## Component Library

### Buttons
1. **Primary** - Filled, primary color, white text
2. **Secondary** - Filled, secondary color, white text
3. **Outlined** - Border only, primary text
4. **Text** - No background, primary text

**Specs**:
- Height: 48dp (minimum touch target)
- Corner radius: 12dp
- Text: 16sp, medium weight
- No caps, no elevation

### Cards
1. **Default** - 16dp corner, 2dp elevation
2. **Elevated** - 16dp corner, 4dp elevation
3. **Interactive** - Ripple effect, clickable
4. **Stat** - 20dp corner, 1dp elevation, 24dp padding

**Specs**:
- Background: Surface color
- Corner radius: 16dp (large), 20dp (stats)
- Elevation: 1-4dp
- Padding: 16-24dp

### Text Inputs
- **Style**: Outlined box
- **Corner radius**: 12dp
- **Min height**: 56dp
- **Stroke**: Primary color when focused
- **Error**: Red color with helper text

### Role Badges
- **Customer**: Blue background, white text
- **Provider**: Green background, white text
- **Admin**: Purple background, white text
- **Corner radius**: Full (pill shape)

### Bottom Navigation (Customer Mode)
- **Selected**: Primary color
- **Unselected**: Tertiary text color
- **Elevation**: 4dp
- **Icons**: 24dp
- **Labels**: 12sp

## Layout Patterns

### Authentication Screens
```
┌─────────────────────────┐
│   Logo/Illustration     │
│                         │
│   Welcome Text          │
│   Subtitle              │
│                         │
│   ┌─────────────────┐   │
│   │ Email Input     │   │
│   └─────────────────┘   │
│   ┌─────────────────┐   │
│   │ Password Input  │   │
│   └─────────────────┘   │
│                         │
│   [Primary Button]      │
│                         │
│   ─── OR ───           │
│                         │
│   [Google Sign In]      │
│                         │
│   Footer Link           │
└─────────────────────────┘
```

### Dashboard (Provider Mode)
```
┌─────────────────────────┐
│ ┌──────┐  Greeting      │
│ │Avatar│  Role Badge    │
│ └──────┘               │
│                         │
│ Stats Row (4 cards)     │
│ ┌───┐ ┌───┐ ┌───┐ ┌───┐│
│ │ # │ │ # │ │ # │ │ # ││
│ └───┘ └───┘ └───┘ └───┘│
│                         │
│ Module Cards (Grid)     │
│ ┌─────────┐ ┌─────────┐│
│ │Service  │ │Billing  ││
│ │Entry    │ │         ││
│ └─────────┘ └─────────┘│
│ ┌─────────┐ ┌─────────┐│
│ │Payment  │ │Reports  ││
│ │         │ │         ││
│ └─────────┘ └─────────┘│
└─────────────────────────┘
```

### Customer View (Bottom Nav)
```
┌─────────────────────────┐
│                         │
│   Screen Content        │
│                         │
│                         │
│                         │
├─────────────────────────┤
│ [Home] [Services] [📞] │
│                         │
└─────────────────────────┘
```

## Accessibility Features

### Touch Targets
- **Minimum**: 48dp × 48dp
- **Recommended**: 56dp × 56dp for primary actions

### Text Contrast
- **Primary text**: 4.5:1 minimum ratio
- **Large text**: 3:1 minimum ratio
- **Icons**: 3:1 minimum ratio

### Font Sizes (Elderly-Friendly)
- Never below 14sp for body text
- Primary actions: 16sp minimum
- Headings: 20sp+ for easy reading
- Line height: 1.5× for readability

## Role-Based UI

### Customer Features
- Bottom navigation (Home, Services, Support)
- Service cards with booking options
- Order tracking
- Payment history

### Provider Features
- Dashboard navigation (grid of modules)
- Service entry forms
- Billing management
- Reports and analytics

### Role Switcher (Future)
- Profile screen toggle
- Seamless context switching
- Badge indication

## Animation Guidelines

### Transitions
- **Duration**: 200-300ms
- **Easing**: ease-in-out
- **Types**: Fade, slide, scale

### Micro-interactions
- Button press: Scale 0.95
- Card tap: Ripple effect
- Loading: Circular progress
- Success: Checkmark animation

## Implementation Status

✅ **Completed**:
- Color palette (colors.xml)
- Dimension system (dimens.xml)
- Text styles (text_styles.xml)
- Widget styles (widget_styles.xml)
- Theme configuration (themes.xml)
- Bottom nav color states

⏳ **Next Steps**:
1. Update login/signup layouts with new styles
2. Redesign dashboard with premium cards
3. Add bottom navigation for customer mode
4. Create role switcher component
5. Implement gradient backgrounds
6. Add micro-animations

## Usage Examples

### Button
```xml
<Button
    style="@style/Widget.App.Button.Primary"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Sign In" />
```

### Card
```xml
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.App.Card.Elevated"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">
    <!-- Content -->
</com.google.android.material.card.MaterialCardView>
```

### Text Input
```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.App.TextInputLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="Email">
    
    <com.google.android.material.textfield.TextInputEditText
        style="@style/Widget.App.TextInputEditText"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="textEmailAddress" />
        
</com.google.android.material.textfield.TextInputLayout>
```

### Typography
```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Welcome Back"
    android:textAppearance="@style/TextAppearance.App.Headline.Large" />
```

## Design Inspiration
- **PhonePe**: Trust blue, clean cards, bottom nav
- **Google Pay**: Fresh colors, smooth animations
- **Swiggy**: Warm orange accents, friendly UI
- **Material Design 3**: Modern components, accessibility

## Resources
- Color tool: material.io/resources/color
- Accessibility: material.io/design/usability/accessibility
- Typography: material.io/design/typography
- Components: material.io/components

---
*Design System v1.0 - Daily Service App*
