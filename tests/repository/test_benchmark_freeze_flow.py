"""Compile-time contracts for benchmark freeze state and persisted owner decisions."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
VM = ROOT / 'app/src/main/java/com/transvibe/app/presentation/TransVibeViewModel.kt'
SCREEN = ROOT / 'app/src/main/java/com/transvibe/app/presentation/screens/BenchmarkScreen.kt'

class BenchmarkFreezeFlowContract(unittest.TestCase):
    def test_public_freeze_records_state_flow_is_declared(self):
        vm = VM.read_text(encoding='utf-8')
        self.assertIn('private val _freezeRecords = MutableStateFlow<Map<String, FreezeRecordEntity>>(emptyMap())',vm)
        self.assertIn('val freezeRecords: StateFlow<Map<String, FreezeRecordEntity>> = _freezeRecords.asStateFlow()',vm)
        self.assertIn('_freezeRecords.value = list.associateBy { it.modelId }',vm)
    def test_ui_reads_persisted_state_not_mock_or_frozen_default(self):
        screen = SCREEN.read_text(encoding='utf-8')
        self.assertIn('viewModel.freezeRecords.collectAsState()',screen)
        self.assertIn('tinyFreeze?.approvedByOwner',screen)
        self.assertIn('tinyFreeze?.status',screen)

if __name__ == '__main__': unittest.main()
