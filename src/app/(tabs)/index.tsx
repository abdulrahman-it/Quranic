import React from 'react';
import {
  StyleSheet,
  View,
  Text,
  ScrollView,
  SafeAreaView,
  StatusBar,
  Alert,
} from 'react-native';
import { SemanticLight } from '../../design-system/theme';
import { PrayerCard } from '../../features/home/components/PrayerCard';
import { ContinueReadingCard } from '../../features/home/components/ContinueReadingCard';
import { AthkarCard } from '../../features/home/components/AthkarCard';
import { QuickActionItem } from '../../features/home/components/QuickActionItem';

export default function HomeScreen() {
  const handleContinueReading = () => {
    Alert.alert(
      'متابعة الورد',
      'الانتقال إلى سورة الكهف - الآية ٢٣ لوردك المعتاد',
      [{ text: 'حسنًا', style: 'default' }]
    );
  };

  const handleAthkarPress = (type: 'morning' | 'evening') => {
    const title = type === 'morning' ? 'أذكار الصباح' : 'أذكار المساء';
    Alert.alert(
      title,
      `فتح شاشة الأذكار لـ ${title} لبدء الورد والعد التكراري`,
      [{ text: 'ابدأ الآن', style: 'default' }]
    );
  };

  const handleQuickAction = (actionTitle: string) => {
    Alert.alert(
      actionTitle,
      `سيتم الانتقال سريعًا لخدمة ${actionTitle}`,
      [{ text: 'موافق', style: 'default' }]
    );
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <StatusBar barStyle="dark-content" backgroundColor={SemanticLight.background} />

      <ScrollView
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
      >
        {/* Cinematic High-End Header */}
        <View style={styles.header}>
          <View style={styles.headerTitles}>
            <Text style={styles.appTitle}>البَوَّابَة الإِسْلَامِيَّة</Text>
            <Text style={styles.welcomeText}>السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللهِ</Text>
          </View>
          <View style={styles.dateBadge}>
            <Text style={styles.dateAr}>٣ ذو الحجة ١٤٤٧ هـ</Text>
            <Text style={styles.dateEn}>June 21, 2026</Text>
          </View>
        </View>

        {/* 1. Next Prayer Card */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>مواقيت الصلاة اليومية</Text>
        </View>
        <PrayerCard
          nextPrayerName="صلاة المغرب"
          nextPrayerTime="07:25 PM"
          timeRemaining="٠٣:١٨:٤٧"
        />

        {/* 2. Continue Reading Card */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>آخر قراءة متوقفة</Text>
        </View>
        <ContinueReadingCard
          surahName="سُورَة الكَهْف"
          verseIndex={23}
          juzIndex={15}
          onPress={handleContinueReading}
        />

        {/* 3. Morning & Evening Athkar Grid Card */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>الأذكار والتحصين اليومي</Text>
        </View>
        <View style={styles.athkarGrid}>
          <AthkarCard
            type="evening"
            title="أذكار المساء"
            subtitle="حفظ وتحصين النفس"
            timeRange="من العصر إلى المغرب"
            onPress={() => handleAthkarPress('evening')}
          />
          <View style={styles.gridSpacer} />
          <AthkarCard
            type="morning"
            title="أذكار الصباح"
            subtitle="بركة وبداية اليوم"
            timeRange="من الفجر إلى الشروق"
            onPress={() => handleAthkarPress('morning')}
          />
        </View>

        {/* 4. Quick Actions section */}
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>الخدمات السريعة</Text>
        </View>
        <View style={styles.actionsGrid}>
          <View style={styles.actionRow}>
            <QuickActionItem
              title="اتجاه القبلة"
              subtitle="تحديد الاتجاه بدقة"
              iconLabel="🧭"
              onPress={() => handleQuickAction('اتجاه القبلة')}
            />
            <View style={styles.gridSpacer} />
            <QuickActionItem
              title="حصن المسلم"
              subtitle="أدعية مأثورة شاملة"
              iconLabel="🕌"
              onPress={() => handleQuickAction('حصن المسلم')}
            />
          </View>

          <View style={[styles.actionRow, styles.secondRow]}>
            <QuickActionItem
              title="المصحف الشريف"
              subtitle="قراءة وتفاسير ميسرة"
              iconLabel="📖"
              onPress={() => handleQuickAction('المصحف')}
            />
            <View style={styles.gridSpacer} />
            <QuickActionItem
              title="المسبحة الذكية"
              subtitle="متابعة ورد الاستغفار"
              iconLabel="📿"
              onPress={() => handleQuickAction('المسبحة الذكية')}
            />
          </View>
        </View>

        {/* Spiritual Quote footer */}
        <View style={styles.footer}>
          <Text style={styles.footerVerse}>“أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ”</Text>
          <Text style={styles.footerInfo}>جميع القراءات والمواقيت متوافقة مع التقويم المعتمد</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    backgroundColor: SemanticLight.background,
  },
  scrollContent: {
    padding: 24,
    paddingBottom: 40,
  },
  header: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
  },
  headerTitles: {
    alignItems: 'flex-end',
  },
  appTitle: {
    fontSize: 12,
    fontWeight: 'bold',
    color: SemanticLight.secondary,
    letterSpacing: 0.5,
    marginBottom: 4,
  },
  welcomeText: {
    fontSize: 20,
    fontWeight: 'bold',
    color: SemanticLight.textPrimary,
  },
  dateBadge: {
    backgroundColor: SemanticLight.card,
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderWidth: 1,
    borderColor: SemanticLight.border,
    alignItems: 'center',
  },
  dateAr: {
    fontSize: 11,
    fontWeight: 'bold',
    color: SemanticLight.primary,
    marginBottom: 2,
  },
  dateEn: {
    fontSize: 10,
    color: SemanticLight.textSecondary,
  },
  sectionHeader: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
    marginTop: 10,
  },
  sectionTitle: {
    fontSize: 15,
    fontWeight: 'bold',
    color: SemanticLight.textPrimary,
    writingDirection: 'rtl',
  },
  athkarGrid: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'stretch',
    marginBottom: 20,
    height: 120,
  },
  actionsGrid: {
    marginBottom: 24,
  },
  actionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  secondRow: {
    marginTop: 12,
  },
  gridSpacer: {
    width: 12,
  },
  footer: {
    alignItems: 'center',
    marginTop: 24,
    paddingVertical: 16,
    borderTopWidth: 1,
    borderTopColor: SemanticLight.border,
  },
  footerVerse: {
    fontSize: 16,
    fontWeight: 'bold',
    color: SemanticLight.primary,
    textAlign: 'center',
    marginBottom: 6,
  },
  footerInfo: {
    fontSize: 11,
    color: SemanticLight.textSecondary,
    textAlign: 'center',
  },
});
