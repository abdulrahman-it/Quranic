import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { SemanticLight } from '../../../design-system/theme';

interface PrayerCardProps {
  nextPrayerName: string;
  nextPrayerTime: string;
  timeRemaining: string;
}

export const PrayerCard: React.FC<PrayerCardProps> = ({
  nextPrayerName,
  nextPrayerTime,
  timeRemaining,
}) => {
  return (
    <View style={styles.container}>
      <View style={styles.topRow}>
        <View style={styles.badge}>
          <Text style={styles.badgeText}>الصلاة القادمة</Text>
        </View>
        <Text style={styles.prayerName}>{nextPrayerName}</Text>
      </View>

      <Text style={styles.prayerTime}>{nextPrayerTime}</Text>

      <View style={styles.bottomRow}>
        <Text style={styles.remainingLabel}>الوقت المتبقي للأذان:</Text>
        <Text style={styles.remainingValue}>{timeRemaining}</Text>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: SemanticLight.surface,
    borderRadius: 20,
    padding: 20,
    borderWidth: 1,
    borderColor: SemanticLight.border,
    marginBottom: 16,
    shadowColor: SemanticLight.shadow,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 1,
    shadowRadius: 12,
    elevation: 3,
  },
  topRow: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  badge: {
    backgroundColor: '#EBF7F0',
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 10,
  },
  badgeText: {
    color: SemanticLight.primary,
    fontSize: 12,
    fontWeight: 'bold',
  },
  prayerName: {
    fontSize: 18,
    color: SemanticLight.textPrimary,
    fontWeight: '900',
    writingDirection: 'rtl',
  },
  prayerTime: {
    fontSize: 32,
    fontWeight: 'normal',
    color: SemanticLight.primary,
    textAlign: 'center',
    marginVertical: 10,
    letterSpacing: 1,
  },
  bottomRow: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 8,
    borderTopWidth: 1,
    borderTopColor: '#F2ECE0',
    paddingTop: 12,
  },
  remainingLabel: {
    fontSize: 13,
    color: SemanticLight.textSecondary,
    writingDirection: 'rtl',
  },
  remainingValue: {
    fontSize: 14,
    color: SemanticLight.secondary,
    fontWeight: 'bold',
    writingDirection: 'rtl',
  },
});
