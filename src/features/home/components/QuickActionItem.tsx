import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { SemanticLight } from '../../../design-system/theme';

interface QuickActionItemProps {
  title: string;
  subtitle: string;
  iconLabel: string; // High-level spiritual representative icon Unicode/Symbol (since we avoid external icon libraries to be self-contained)
  onPress: () => void;
}

export const QuickActionItem: React.FC<QuickActionItemProps> = ({
  title,
  subtitle,
  iconLabel,
  onPress,
}) => {
  return (
    <TouchableOpacity style={styles.container} activeOpacity={0.85} onPress={onPress}>
      <View style={styles.content}>
        <View style={styles.textWrapper}>
          <Text style={styles.title}>{title}</Text>
          <Text style={styles.subtitle}>{subtitle}</Text>
        </View>
        <View style={styles.iconCircle}>
          <Text style={styles.icon}>{iconLabel}</Text>
        </View>
      </View>
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  container: {
    backgroundColor: SemanticLight.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: SemanticLight.border,
    padding: 14,
    flex: 1,
    minWidth: '45%',
    shadowColor: SemanticLight.shadow,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 1,
    shadowRadius: 6,
    elevation: 2,
  },
  content: {
    flexDirection: 'row-reverse',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  textWrapper: {
    alignItems: 'flex-end',
    flex: 1,
    paddingLeft: 8,
  },
  title: {
    fontSize: 14,
    color: SemanticLight.textPrimary,
    fontWeight: 'bold',
    writingDirection: 'rtl',
    marginBottom: 2,
  },
  subtitle: {
    fontSize: 11,
    color: SemanticLight.textSecondary,
    writingDirection: 'rtl',
  },
  iconCircle: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: '#F7F6F1',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: '#EFECE2',
  },
  icon: {
    fontSize: 18,
  },
});
