import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { SemanticLight } from '../../../design-system/theme';

interface QuranLastReadCardProps {
  surahName: string;
  surahId: number;
  ayahNumber: number;
  onPress: () => void;
}

export const QuranLastReadCard: React.FC<QuranLastReadCardProps> = ({
  surahName,
  surahId,
  ayahNumber,
  onPress,
}) => {
  return (
    <TouchableOpacity style={styles.container} activeOpacity={0.85} onPress={onPress}>
      <View style={styles.header}>
        <View style={styles.badge}>
          <Text style={styles.badgeText}>آخر قراءة</Text>
        </View>
        <Text style={styles.headingAr}>تابع من حيث توقفت</Text>
      </View>

      <View style={styles.body}>
        <View style={styles.textColumn}>
          <Text style={styles.surahTitle}>{surahName}</Text>
          <Text style={styles.metaText}>السورة رقم {surahId} • الآية الكريمة {ayahNumber}</Text>
        </View>

        {/* Elegant visual indicator */}
        <View style={styles.circleIndicator}>
          <Text style={styles.indicatorText}>📖</Text>
        </View>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: SemanticLight.card,
    borderRadius: 20,
    borderColor: SemanticLight.border,
    borderWidth: 1,
    padding: 18,
    marginBottom: 20,
  },
  header: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  badge: {
    backgroundColor: SemanticLight.secondary,
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 8,
  },
  badgeText: {
    color: SemanticLight.white,
    fontSize: 10,
    fontWeight: 'bold',
  },
  headingAr: {
    fontSize: 12,
    fontWeight: 'bold',
    color: SemanticLight.textSecondary,
    writingDirection: 'rtl',
  },
  body: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  textColumn: {
    alignItems: 'flex-end',
  },
  surahTitle: {
    fontSize: 22,
    color: SemanticLight.primary,
    fontWeight: 'bold',
    writingDirection: 'rtl',
    marginBottom: 4,
  },
  metaText: {
    fontSize: 13,
    color: SemanticLight.textSecondary,
    writingDirection: 'rtl',
  },
  circleIndicator: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: '#FAF7F0',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: '#EFECE2',
  },
  indicatorText: {
    fontSize: 20,
  },
});
