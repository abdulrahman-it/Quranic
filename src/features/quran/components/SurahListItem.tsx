import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { SemanticLight } from '../../../design-system/theme';

interface SurahListItemProps {
  id: number;
  nameAr: string;
  nameEn: string;
  type: string; // مكة / المدينة
  verseCount: number;
  onPress: () => void;
}

export const SurahListItem: React.FC<SurahListItemProps> = ({
  id,
  nameAr,
  nameEn,
  type,
  verseCount,
  onPress,
}) => {
  return (
    <TouchableOpacity style={styles.container} activeOpacity={0.8} onPress={onPress}>
      <View style={styles.leftRow}>
        <Text style={styles.surahEn}>{nameEn}</Text>
        <Text style={styles.details}>{verseCount} آية • {type}</Text>
      </View>

      <View style={styles.rightRow}>
        <View style={styles.nameContainer}>
          <Text style={styles.surahAr}>{nameAr}</Text>
        </View>

        {/* Beautiful luxury geometric border/badge for the surah number */}
        <View style={styles.numberBadge}>
          <Text style={styles.numberText}>{id}</Text>
        </View>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: SemanticLight.surface,
    borderColor: SemanticLight.border,
    borderWidth: 1,
    borderRadius: 16,
    padding: 16,
    marginBottom: 10,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    shadowColor: SemanticLight.shadow,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 1,
    shadowRadius: 6,
    elevation: 2,
  },
  leftRow: {
    alignItems: 'flex-start',
  },
  surahEn: {
    fontSize: 14,
    fontWeight: 'bold',
    color: SemanticLight.primary,
    marginBottom: 4,
  },
  details: {
    fontSize: 11,
    color: SemanticLight.textSecondary,
  },
  rightRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
  },
  nameContainer: {
    marginRight: 14,
    alignItems: 'flex-end',
  },
  surahAr: {
    fontSize: 20,
    fontWeight: 'bold',
    color: SemanticLight.textPrimary,
    writingDirection: 'rtl',
  },
  numberBadge: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#FAF7F0',
    borderColor: SemanticLight.secondary,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  numberText: {
    fontSize: 12,
    fontWeight: 'bold',
    color: SemanticLight.secondary,
  },
});
