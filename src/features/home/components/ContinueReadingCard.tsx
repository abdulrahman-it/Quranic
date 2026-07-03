import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { SemanticLight } from '../../../design-system/theme';

interface ContinueReadingCardProps {
  surahName: string;
  verseIndex: number;
  juzIndex: number;
  onPress: () => void;
}

export const ContinueReadingCard: React.FC<ContinueReadingCardProps> = ({
  surahName,
  verseIndex,
  juzIndex,
  onPress,
}) => {
  return (
    <TouchableOpacity style={styles.container} activeOpacity={0.85} onPress={onPress}>
      <View style={styles.header}>
        <Text style={styles.title}>متابعة القراءة والورد اليومي</Text>
        <View style={styles.indicator} />
      </View>

      <View style={styles.content}>
        <View style={styles.textWrapper}>
          <Text style={styles.surahName}>{surahName}</Text>
          <Text style={styles.details}>الآية {verseIndex} • الجزء {juzIndex}</Text>
        </View>

        <View style={styles.actionBtn}>
          <Text style={styles.actionText}>استكمل الآن</Text>
        </View>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: SemanticLight.card,
    borderRadius: 20,
    padding: 20,
    borderWidth: 1,
    borderColor: SemanticLight.border,
    marginBottom: 16,
  },
  header: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 14,
  },
  title: {
    fontSize: 13,
    color: SemanticLight.textSecondary,
    fontWeight: 'bold',
    writingDirection: 'rtl',
  },
  indicator: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: SemanticLight.secondary,
  },
  content: {
    flexDirection: 'row-reverse',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  textWrapper: {
    alignItems: 'flex-end',
  },
  surahName: {
    fontSize: 22,
    color: SemanticLight.primary,
    fontWeight: '800',
    writingDirection: 'rtl',
    marginBottom: 4,
  },
  details: {
    fontSize: 13,
    color: SemanticLight.textSecondary,
    writingDirection: 'rtl',
  },
  actionBtn: {
    backgroundColor: SemanticLight.primary,
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 12,
  },
  actionText: {
    color: SemanticLight.white,
    fontSize: 12,
    fontWeight: 'bold',
    writingDirection: 'rtl',
  },
});
